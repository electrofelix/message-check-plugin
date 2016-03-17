package com.googlesource.gerrit.plugins.messagecheckplugin;

import com.google.gerrit.common.data.RefConfigSection;

public class MatcherSection extends RefConfigSection implements Comparable<MatcherSection>{

  protected String pattern;
  public enum type {
    REGEX, GLOB, PLAIN;

    public String toString(){
      switch(this){
      case REGEX :
          return "regex";
      case GLOB :
          return "glob";
      case PLAIN :
          return "plain";
      }
      return null;
    }
  }

  public MatcherSection(String refPattern) {
    super(refPattern);
  }

  @Override
  public int compareTo(MatcherSection o) {
    return comparePattern().compareTo(o.comparePattern());
  }

  private String comparePattern() {
    if (getName().startsWith(REGEX_PREFIX)) {
      return getName().substring(REGEX_PREFIX.length());
    }
    return getName();
  }

  @Override
  public String toString() {
    return "MatcherSection[" + getName() + "]";
  }
}
