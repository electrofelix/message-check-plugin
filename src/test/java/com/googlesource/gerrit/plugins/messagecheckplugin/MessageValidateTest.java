package com.googlesource.gerrit.plugins.messagecheckplugin;

import static org.easymock.EasyMock.expect;

import java.util.List;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.ReceiveCommand;

import org.junit.runner.RunWith;
import org.junit.Before;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.FactoryModule;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.googlesource.gerrit.plugins.messagecheckplugin.testutil.MockingTestCase;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RevCommit.class})
public class MessageValidateTest extends MockingTestCase {

  private PluginConfigFactory pluginConfigFactory;
  private Config pluginConfig;
  private Injector injector;
  private Project project = new Project(new Project.NameKey("myProject"));

  private final String commitmessage = "Dummy commit subject\n"
      + "\n"
      + "Start of commit message body\n"
      + "\n"
      + "Commit Footer\n"
      + "ChangeId: I9e286773a7d90ddd2e664a4695ca4f635bbeb0b9";

  public void testNonMatching() throws CommitValidationException {
    List<CommitValidationMessage> ret;
    MessageValidator mvc = injector.getInstance(MessageValidator.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = new CommitReceivedEvent(command, project, null,
        commit, null);

    expect(pluginConfig.getStringList("branch", null, "pattern"))
        .andReturn(new String[] {"\"Non matching pattern\""});
    expect(commit.getFullMessage()).andReturn(commitmessage);

    replayMocks();

    ret = mvc.onCommitReceived(event);

    assertEquals("Expected one CommitValidationMessages", 1, ret.size());
    assertTrue("First CommitValidationMessages does not contain " +
        "'Commit message failed to match against'",
        ret.get(0).getMessage().contains("Commit message failed to match against"));
  }

  public void testMatchAll() throws CommitValidationException {
    List<CommitValidationMessage> ret;
    MessageValidator mvc = injector.getInstance(MessageValidator.class);
    ReceiveCommand command = createMock(ReceiveCommand.class);
    RevCommit commit = createMock(RevCommit.class);
    CommitReceivedEvent event = new CommitReceivedEvent(command, project, null,
        commit, null);

    expect(pluginConfig.getStringList("branch", null, "pattern"))
        .andReturn(new String[] {"\"(?s)^Dummy commit subject\n\n.*Commit Footer\""});
    expect(commit.getFullMessage()).andReturn(commitmessage);

    replayMocks();

    ret = mvc.onCommitReceived(event);

    assertEquals("Expected no CommitValidationMessages", 0, ret.size());
  }

  public void assertEmptyList(List<CommitValidationMessage> list) {
    if (!list.isEmpty()) {
      StringBuffer sb = new StringBuffer();
      sb.append("Commit Validation List is not emptyList is not empty, but contains:\n");
      for (CommitValidationMessage msg : list) {
        sb.append(msg.getMessage());
        sb.append("\n");
      }
      fail(sb.toString());
    }
  }

  private void setupCommonMocks() throws NoSuchProjectException {
    expect(pluginConfigFactory.getProjectPluginConfigWithInheritance(
        project.getNameKey(), "message-check-plugin"))
            .andReturn(pluginConfig).anyTimes();
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();

    injector = Guice.createInjector(new TestModule());

    setupCommonMocks();
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      bind(String.class).annotatedWith(PluginName.class)
           .toInstance("MCPTestName");

      pluginConfigFactory = createMock(PluginConfigFactory.class);
      bind(PluginConfigFactory.class).toInstance(pluginConfigFactory);

      pluginConfig = createMock(Config.class);
      bind(Config.class).toInstance(pluginConfig);
    };
  }
}
