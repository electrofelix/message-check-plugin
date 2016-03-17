package com.googlesource.gerrit.plugins.messagecheckplugin;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Listen
@Singleton
public class MessageValidator implements CommitValidationListener {

  private class patternMatcher {
    public final Pattern pattern;
    public final String position;
    public final Boolean failOnMissing;

    patternMatcher(final String line) throws ConfigInvalidException {
      // string to be matched on is always enclosed in quotes, no other options
      // that control how this pattern is applied should ever use quotes
      int regexEnd = line.lastIndexOf("\"");
      String regex = line.substring(1, regexEnd);
      try {
        pattern = Pattern.compile(regex);
      } catch(PatternSyntaxException e) {
        throw new ConfigInvalidException("Invalid Regex pattern: \"" + regex + "\".", e);
      }

      String[] options = line.substring(regexEnd+1).split(" ");
      switch(options.length) {
        case 1:     position = "all";
                    failOnMissing = true;
                    break;
        case 2:     position = options[1];
                    failOnMissing = true;
                    break;
        case 3:     position = options[1];
                    failOnMissing = Boolean.valueOf(options[2]);
                    break;
        default:    throw new ConfigInvalidException("Invalid format: should be \"<pattern> [<position> [<fail-on-missing>]]\"");
      }

      // lets keep it simple for now and extend
      if(!position.equals("all")) {
        throw new ConfigInvalidException("Position \"" + position + "\" not supported yet, only \"all\" is valid, seen " + line
            + ", with regexEnd of " + regexEnd + " and options.length = " + options.length);
      }
    }

    Boolean matches(final RevCommit commit) {
      return pattern.matcher(commit.getFullMessage()).find();
    }
  }

  private static Logger log = LoggerFactory.getLogger(MessageValidator.class);

  @Inject
  private com.google.gerrit.server.config.PluginConfigFactory cfg;

  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {

    final RevCommit commit = receiveEvent.commit;
    final Project project = receiveEvent.project;
    final List<CommitValidationMessage> messages = new ArrayList<>();

    patternMatcher matcher;

    /*
     * should reading of config and building of list of matchers be done only when
     * config is updated and outside of this function?
     */
    String[] patterns;
    try {
      // Currently this only handles full names of branches, but it should
      // support the same regex branch patterns supported by Gerrit
      patterns = cfg.getProjectPluginConfigWithInheritance(project.getNameKey(), "message-check-plugin")
          .getStringList("branch", receiveEvent.refName, "pattern");
    } catch(NoSuchProjectException e) {
      throw new CommitValidationException("Project does not exist, cannot retrieve configuration.", e);
    }

    for(String pattern: patterns) {
      try {
        matcher = new patternMatcher(pattern);
      } catch(ConfigInvalidException e) {
        String msg = "Problem parsing branch." + receiveEvent.refName + ".pattern\n";
        msg += "  " + e.getMessage() + "\n";
        messages.add(new CommitValidationMessage(msg, false));
        continue;
      }

      if(!matcher.matches(commit)) {
        String msg = "Commit message failed to match against \"" + matcher.pattern + "\"\n";
        messages.add(new CommitValidationMessage(msg, matcher.failOnMissing));
      } else {
        log.debug("Commit message matched against: \"" + matcher.pattern + "\"");
      }
    }

    return messages;
  }
}
