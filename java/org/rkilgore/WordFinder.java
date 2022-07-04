package org.rkilgore;

import java.util.HashMap;
import java.util.Map;


public class WordFinder {

  public WordFinder(String dictfilename) {
    this._dict = new TrieNode(dictfilename);
  }


  public Map<String, String> findWords(String letters, String template, int maxPrefix, int maxPostfix) {
    String requiredLetters = calcRequiredLetters(letters);
    letters = letters.toLowerCase();
    this._requiredLetters = requiredLetters;
    this._maxPrefix = maxPrefix;
    this._maxPostfix = maxPostfix;
    this._words = new HashMap<String, String>();
    recurse(0 /* depth */, "" /* sofar */, "" /* dotsSoFar */,
            letters, template, false /* templateStarted */,
            0 /* curPrefixLen */, 0 /* curPostfixLen */);
    return this._words;
  }


  private void recurse(
      int depth,
      String sofar,
      String dotsSoFar,
      String letters,
      String template,
      boolean templateStarted,
      int curPrefixLen,
      int curPostfixLen) {

    debugLog(String.format("%srecurse sofar=%s dotsSoFar=%s letters=%s template=%s prefix=%d postfix=%d "
                           + "templStarted=%s",
                           new String(new char[depth*2]).replace((char)0, ' '),
                           sofar, dotsSoFar, letters, template, curPrefixLen, curPostfixLen,
                           String.valueOf(templateStarted)));

    boolean nextTemplateIsLetter = !template.isEmpty() && template.charAt(0) != '.';
    boolean cantAddPostfix = curPostfixLen == this._maxPostfix;
    if ((letters.isEmpty() && !nextTemplateIsLetter) ||
        (template.isEmpty() && cantAddPostfix)) {
      debugLog(String.format("    short return: sofar=%s letters=%s templ=%s", sofar, letters, template));
      return;
    }


    if (!template.isEmpty()) {
      // try adding ch from letters to prefix before template
      int remainingLettersNeeded = (int) template.chars().filter(
          ch -> ((char) ch) == '.' || Character.isUpperCase((char) ch)).count();
      if (!templateStarted && curPrefixLen < this._maxPrefix && letters.length() > remainingLettersNeeded) {
        debugLog(String.format("%s    prefix: remaining = %d for sofar=%s letters=%s templ=%s",
                               new String(new char[depth*2]).replace((char)0, ' '),
                               sofar, letters, template));
        for (char ch : rmDupes(letters).toCharArray()) {
          String newletters = letters.replaceFirst(String.valueOf(ch), "");
          boolean isDot = ch == '.';
          char[] searchChars = isDot ? "abcdefghijklmnopqrstuvwxyz".toCharArray() : new char[] { ch };
          for (char sch : searchChars) {
            String nextsofar = sofar + sch;
            String nextDotsSoFar = dotsSoFar + (isDot ? String.valueOf(sch) : "");
            TrieResult result = _dict.isPrefix(nextsofar);
            if (result.isword && template.isEmpty() && hasRequiredLetters(nextsofar)) {
              addWord(nextsofar, nextDotsSoFar);
            } else if (result.isprefix) {
              // Map<String, String> rwords =
                recurse(depth+1, nextsofar, nextDotsSoFar,
                        newletters, template, false, curPrefixLen+1, curPostfixLen);
              /*
              for (String word : rwords.keySet()) {
                addWord(word, rwords[word]);
              }
              */
            }
          }
        }
      }
    }


    // template letter = '.' - try each letter from letters as match
    if (!template.isEmpty() && template.charAt(0) == '.') {
      debugLog(String.format("%s    template dot: sofar=%s letters=%s templ=%s",
                             new String(new char[depth*2]).replace((char)0, ' '),
                             sofar, letters, template));
      for (char ch : rmDupes(letters).toCharArray()) {
        String newletters = letters.replaceFirst(String.valueOf(ch), "");
        String newtemplate = template.substring(1);
        boolean isDot = ch == '.';
        char[] searchChars = isDot ? "abcdefghijklmnopqrstuvwxyz".toCharArray() : new char[] { ch };
        for (char sch : searchChars) {
          String nextsofar = sofar + sch;
          String nextDotsSoFar = dotsSoFar + (isDot ? String.valueOf(sch) : "");
          TrieResult result = this._dict.isPrefix(nextsofar);
          if (result.isword && newtemplate.isEmpty() && hasRequiredLetters(nextsofar)) {
            addWord(nextsofar, nextDotsSoFar);
          }
          if (result.isprefix) {
            recurse(depth+1, nextsofar, nextDotsSoFar,
                    newletters, newtemplate, true, curPrefixLen, curPostfixLen);
          }
        }
      }
    } else if (!template.isEmpty()) {
      // template letter ch != '.' - add ch.lower() and remove ch.lower() from input letters if ch.isupper()
      debugLog(String.format("%s    template NON-dot: sofar=%s letters=%s templ=%s",
                             new String(new char[depth*2]).replace((char)0, ' '),
                             sofar, letters, template));

      // if template character is uppercase, use letter from letters
      char ch = template.charAt(0);
      if (Character.isUpperCase(ch)) {
        ch = Character.toLowerCase(ch);
        if (letters.indexOf(ch) == -1) {
          return;
        }
        letters = letters.replaceFirst(String.valueOf(ch), "");
      }
      String nextsofar = sofar + ch;
      String newtemplate = template.substring(1);
      TrieResult result = this._dict.isPrefix(nextsofar);
      if (result.isword && newtemplate.isEmpty() && hasRequiredLetters(nextsofar)) {
        addWord(nextsofar, dotsSoFar);
      }
      if (result.isprefix) {
        recurse(depth+1, nextsofar, dotsSoFar,
                letters, newtemplate, true, curPrefixLen, curPostfixLen);
      }
    } else if (curPostfixLen < this._maxPostfix) {
      // at end, add letters for the postfix
      debugLog(String.format("%s    postfix: sofar=%s letters=%s templ=%s",
                             new String(new char[depth*2]).replace((char)0, ' '),
                             sofar, letters, template));
      for (char ch : rmDupes(letters).toCharArray()) {
        String newletters = letters.replaceFirst(String.valueOf(ch), "");
        boolean isDot = ch == '.';
        char[] searchChars = isDot ? "abcdefghijklmnopqrstuvwxyz".toCharArray() : new char[] { ch };
        for (char sch : searchChars) {
          String nextsofar = sofar + sch;
          String nextDotsSoFar = dotsSoFar + (isDot ? String.valueOf(sch) : "");
          TrieResult result = this._dict.isPrefix(nextsofar);
          if (result.isword && hasRequiredLetters(nextsofar)) {
            addWord(nextsofar, nextDotsSoFar);
          }
          if (result.isprefix) {
            recurse(depth+1, nextsofar, nextDotsSoFar,
                    newletters, template, true, curPrefixLen, curPostfixLen+1);
          }
        }
      }
    }
  }

  private void debugLog(String msg) {
    if (DEBUG) {
      System.out.println(msg);
    }
  }

  private String rmDupes(String letters) {
    StringBuilder sb = new StringBuilder();
    char[] chars = letters.toCharArray();
    for (char ch : chars) {
      if (sb.indexOf(String.valueOf(ch)) == -1) {
        sb.append(ch);
      }
    }
    return sb.toString();
  }


  private String calcRequiredLetters(String letters) {
    StringBuilder sb = new StringBuilder();
    char[] chars = letters.toCharArray();
    for (char ch : chars) {
      if (ch != '.' && Character.isUpperCase(ch)) {
        sb.append(Character.toLowerCase(ch));
      }
    }
    return sb.toString();
  }


  private boolean hasRequiredLetters(String word) {
    char[] chars = this._requiredLetters.toCharArray();
    for (char ch : chars) {
      if (word.indexOf(ch) == -1) {
        return false;
      }
    }
    return true;
  }

  private void addWord(String word, String dotVals) {
    debugLog(String.format("        add_word(%s, %s)", word, dotVals));
    String curDotVals = this._words.get(word);
    this._words.put(word, curDotVals == null ? dotVals : curDotVals + ":" + dotVals);
  }


  private static void reportTime(String msg) {
    long now = System.currentTimeMillis();
    if (WordFinder._lastTime > 0) {
      System.out.printf("%s: elapsed = %.3f\n", msg, 1.0 * (now - WordFinder._lastTime) / 1000.0);
    } else {
      System.out.printf("%s: time = 0\n", msg);
    }
    WordFinder._lastTime = now;
  }


  public static void main(String[] args) {
    WordFinder.reportTime("laoding dictionary...");
    WordFinder wf = new WordFinder("./scrabble_words.txt");
    WordFinder.reportTime("loaded.");

    Map<String, String> words = wf.findWords("oval", "", 7, 7);
    WordFinder.reportTime("findWords complete.");

    for (String word : words.keySet()) {
      String dotVals = words.get(word);
      System.out.println(String.format("%s%s", dotVals.isEmpty() ? "" : dotVals+": ", word));
    }
  }

  private static boolean DEBUG = false;
  private TrieNode _dict;
  private Map<String, String> _words;
  private String _requiredLetters;
  private int _maxPrefix;
  private int _maxPostfix;
  private static long _lastTime = 0;
}