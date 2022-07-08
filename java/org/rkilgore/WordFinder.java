package org.rkilgore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import static java.util.Map.entry;


/**
 * This class is currently NOT thread-safe because of the following
 * instance variables used for a single search:
 *    _requiredLetters
 *    _maxPrefix
 *    _maxPostfix
 *    _words
 */
public class WordFinder {

  static class Tile {
    public static Tile EMPTY = new Tile("empty");
    public static Tile DLETTER = new Tile("DL", 2, 1);
    public static Tile TLETTER = new Tile("TL", 3, 1);
    public static Tile DWORD = new Tile("DW", 1, 2);
    public static Tile TWORD = new Tile("TW", 1, 3);
    public static Tile forLetter(char letter) {
      return new Tile(letter);
    }
    public static Tile forEmptyUseLetter(char letter) {
      Tile tile = forLetter(letter);
      tile.empty = true;
      return tile;
    }

    Tile(String name) {
      this.name = name;
      this.empty = true;
      this.letter = (char) 0;
      this.letterMult = 1;
      this.wordMult = 1;
    }

    Tile(char letter) {
      this("letter " + letter);
      this.empty = false;
      this.letter = letter;
    }

    Tile(String name, int letterMult, int wordMult) {
      this(name);
      this.letterMult = letterMult;
      this.wordMult = wordMult;
    }

    public String toString() {
      return this.name;
    }

    public boolean hasLetter() {
      return this.letter != (char) 0;
    }

    public String name;
    public boolean empty;
    public char letter;
    public int letterMult;
    public int wordMult;
  }

  enum LetterPlacement {
    PREFIX,
    TEMPLATE,
    POSTFIX
  }

  static Map<Character, Integer> letterScores = Map.ofEntries(
      entry('a', 1), entry('b', 3), entry('c', 3), entry('d', 2),
      entry('e', 1), entry('f', 4), entry('g', 3), entry('h', 4),
      entry('i', 1), entry('j', 10), entry('k', 5), entry('l', 1),
      entry('m', 3), entry('n', 2), entry('o', 1), entry('p', 3),
      entry('q', 11), entry('r', 1), entry('s', 1), entry('t', 1),
      entry('u', 3), entry('v', 4), entry('w', 4), entry('x', 8),
      entry('y', 4), entry('z', 11)
  );


  static class WordInfo {
    public WordInfo(int score, String dotVals) {
      this.score = score;
      this.dotVals = dotVals;
    }

    public int score;
    public String dotVals;
  }


  public WordFinder(String dictfilename) {
    this._dict = new TrieNode(dictfilename);
  }

  public WordFinder(Scanner scanner) {
    this._dict = new TrieNode(scanner);
  }


  public Map<String, WordInfo> findWords(String letters, String template, int maxPrefix, int maxPostfix) {

    if (!template.isEmpty()) {
      if (Character.isDigit(template.charAt(0))) {
        maxPrefix = Character.digit(template.charAt(0), 10);
        System.out.println("maxPrefix = " + maxPrefix);
        template = template.substring(1);
      }
      int last = template.length() - 1;
      if (Character.isDigit(template.charAt(last))) {
        maxPostfix = Character.digit(template.charAt(last), 10);
        System.out.println("maxPostfix = " + maxPostfix);
        template = template.substring(0, last);
      }
    }

    List<Tile> tiles = new ArrayList<Tile>();
    for (char ch : template.toCharArray()) {
      if (ch == '.') {
        tiles.add(Tile.EMPTY);
      } else if (ch == ',') {
        tiles.add(Tile.TLETTER);
      } else if (ch == '_') {
        tiles.add(Tile.DWORD);
      } else if (Character.isUpperCase(ch)) {
        tiles.add(Tile.forEmptyUseLetter(Character.toLowerCase(ch)));
      } else if (Character.isLetter(ch)) {
        tiles.add(Tile.forLetter(ch));
      } else {
        throw new RuntimeException(String.format("Unrecognized character in template: '%c'", ch));
      }
    }

    String requiredLetters = calcRequiredLetters(letters);
    letters = letters.toLowerCase();
    this._requiredLetters = requiredLetters;
    this._maxPrefix = maxPrefix;
    this._maxPostfix = maxPostfix;
    this._words = new HashMap<String, WordInfo>();
    recurse(0 /* depth */, "" /* sofar */, "" /* dotsSoFar */,
            0 /* scoreSoFar */, 1 /* wordMultSoFar */,
            letters, tiles, false /* templateStarted */,
            0 /* curPrefixLen */, 0 /* curPostfixLen */);
    return this._words;
  }


  private void recurse(
      int depth,
      String sofar,
      String dotsSoFar,
      int scoreSoFar,
      int wordMultSoFar,
      String letters,
      List<Tile> template,
      boolean templateStarted,
      int curPrefixLen,
      int curPostfixLen) {

    debugLog(String.format("%srecurse sofar=%s dotsSoFar=%s letters=%s template=%s prefix=%d postfix=%d "
                           + "templStarted=%s",
                           "  ".repeat(depth),
                           sofar, dotsSoFar, letters, template, curPrefixLen, curPostfixLen,
                           String.valueOf(templateStarted)));

    boolean nextTemplateIsLetter = !template.isEmpty() && !template.get(0).empty;
    boolean cantAddPostfix = curPostfixLen == this._maxPostfix;
    if ((letters.isEmpty() && !nextTemplateIsLetter) ||
        (template.isEmpty() && cantAddPostfix)) {
      debugLog(String.format("    short return: sofar=%s letters=%s templ=%s", sofar, letters, template));
      return;
    }


    if (!template.isEmpty()) {
      // try adding ch from letters to prefix before template
      int remainingLettersNeeded = (int) template.stream().filter(tile -> tile.empty).count();
      if (!templateStarted && curPrefixLen < this._maxPrefix && letters.length() > remainingLettersNeeded) {
        debugLog(String.format("%s    prefix: remaining = %d for sofar=%s letters=%s templ=%s",
                               "  ".repeat(depth),
                               remainingLettersNeeded,
                               sofar, letters, template));

        addLetterFromLettersAndRecurse(
            depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar,
            letters, template, curPrefixLen, curPostfixLen, LetterPlacement.PREFIX);
      }
    }


    if (!template.isEmpty() && template.get(0).empty) {
      // empty tile - two types
      Tile nextTile = template.get(0);
      if (nextTile.hasLetter()) {
        // empty tile for which user has requested to put one of their
        // letters in this spot
        debugLog(String.format("%s   empty tile - take '%c' from letters: sofar=%s letters=%s templ=%s",
                               "  ".repeat(depth),
                               nextTile.letter, sofar, letters, template));
        char ch = nextTile.letter;
        if (letters.indexOf(ch) == -1) {
          // can't fulfill this request
          return;
        }
        letters = letters.replaceFirst(String.valueOf(ch), "");
        addLetterFromTemplateAndRecurse(
            depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar,
            letters, template, curPrefixLen, curPostfixLen, true);

      } else {
        // plain old empty tile
        debugLog(String.format("%s    empty tile: sofar=%s letters=%s templ=%s",
                               "  ".repeat(depth),
                               sofar, letters, template));

        addLetterFromLettersAndRecurse(
            depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar,
            letters, template, curPrefixLen, curPostfixLen, LetterPlacement.TEMPLATE);
      }
    } else if (!template.isEmpty()) {
      // template letter tile - add letter from template
      addLetterFromTemplateAndRecurse(
            depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar,
            letters, template, curPrefixLen, curPostfixLen, false);

    } else if (curPostfixLen < this._maxPostfix) {
      // at end, add letters for the postfix
      debugLog(String.format("%s    postfix: sofar=%s letters=%s templ=%s",
                             "  ".repeat(depth),
                             sofar, letters, template));

      addLetterFromLettersAndRecurse(
          depth, sofar, dotsSoFar, scoreSoFar, wordMultSoFar,
          letters, template, curPrefixLen, curPostfixLen, LetterPlacement.POSTFIX);
    }
  }

  private void addLetterFromTemplateAndRecurse(
        int depth, String sofar, String dotsSoFar,
        int scoreSoFar, int wordMultSoFar,
        String letters, List<Tile> template,
        int curPrefixLen, int curPostfixLen, boolean usingLetter) {
    Tile nextTile = template.get(0);
    char ch = nextTile.letter;
    String nextsofar = sofar + ch;
    List<Tile> newtemplate = template.subList(1, template.size());
    int nextScore = usingLetter ? scoreSoFar + (letterScores.get(ch) * template.get(0).letterMult) : scoreSoFar;
    int nextWordMult = usingLetter ? (wordMultSoFar * template.get(0).wordMult) : wordMultSoFar;
    TrieResult result = this._dict.isPrefix(nextsofar);
    if (result.isword && newtemplate.isEmpty() && hasRequiredLetters(nextsofar)) {
      addWord(nextsofar, nextScore * nextWordMult, dotsSoFar);
    }
    if (result.isprefix) {
      recurse(depth+1, nextsofar, dotsSoFar, nextScore, nextWordMult,
              letters, newtemplate, true, curPrefixLen, curPostfixLen);
    }
  }

  private void addLetterFromLettersAndRecurse(
        int depth, String sofar, String dotsSoFar,
        int scoreSoFar, int wordMultSoFar,
        String letters, List<Tile> template,
        int curPrefixLen, int curPostfixLen, LetterPlacement placement) {
    for (char ch : rmDupes(letters).toCharArray()) {
      boolean isDot = ch == '.';
      String newletters = letters.replaceFirst(isDot ? "\\." : String.valueOf(ch), "");
      List<Tile> newtemplate = placement == LetterPlacement.TEMPLATE ? template.subList(1, template.size()) : template;
      int nextPre = placement == LetterPlacement.PREFIX ? curPrefixLen + 1 : curPrefixLen;
      int nextPost = placement == LetterPlacement.POSTFIX ? curPostfixLen + 1 : curPostfixLen;
      boolean nextTemplateStarted = placement != LetterPlacement.PREFIX;

      char[] searchChars = isDot ? "abcdefghijklmnopqrstuvwxyz".toCharArray() : new char[] { ch };
      for (char sch : searchChars) {
        String nextsofar = sofar + sch;
        String nextDotsSoFar = dotsSoFar + (isDot ? String.valueOf(sch) : "");
        int nextScore = scoreSoFar + (letterScores.get(sch) * (template.size() > 0 ? template.get(0).letterMult : 1));
        int nextWordMult = wordMultSoFar * (template.size() > 0 ? template.get(0).wordMult : 1);
        TrieResult result = this._dict.isPrefix(nextsofar);
        if (result.isword && newtemplate.isEmpty() && hasRequiredLetters(nextsofar)) {
          addWord(nextsofar, nextScore * nextWordMult, nextDotsSoFar);
        }
        if (result.isprefix) {
          recurse(depth+1, nextsofar, nextDotsSoFar, nextScore, nextWordMult,
                  newletters, newtemplate, nextTemplateStarted, nextPre, nextPost);
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

  private void addWord(String word, int score, String dotVals) {
    debugLog(String.format("        add_word(%s, %s)", word, dotVals));
    WordInfo prev = this._words.get(word);
    if (prev == null || prev.score < score) {
      this._words.put(word, new WordInfo(score, dotVals));
    } else if (prev == null || dotVals.length() < prev.dotVals.length()) {
      this._words.put(word, new WordInfo(score, dotVals));
    }
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
    String letters = args[0];
    String template = args.length > 1 ? args[1] : "";
    int maxPrefix = args.length > 2 ? Integer.valueOf(args[2]) : 7;
    int maxPostfix = args.length > 3 ? Integer.valueOf(args[3]) : 7;

    WordFinder.reportTime("laoding dictionary...");
    WordFinder wf = new WordFinder("./scrabble_words.txt");
    WordFinder.reportTime("loaded.");

    Map<String, WordInfo> map = wf.findWords(letters, template, maxPrefix, maxPostfix);
    WordFinder.reportTime("findWords complete.");

    List<String> words = new ArrayList<>(map.keySet());
    words.sort((a, b) -> {
        WordInfo wia = map.get(a);
        WordInfo wib = map.get(b);
        if (wia.score != wib.score) {
          return wia.score - wib.score;
        }
        if (a.length() != b.length()) {
          return a.length() - b.length();
        }
        if (wia.dotVals.length() != wib.dotVals.length()) {
          return wia.dotVals.length() - wib.dotVals.length();
        }
        return a.compareTo(b);
    });
    for (String word : words) {
      if (!word.equals(template.toLowerCase().replaceAll("\\d", ""))) {
        WordInfo winfo = map.get(word);
        System.out.println(String.format("%s%s score:%d", winfo.dotVals.isEmpty() ? "" : winfo.dotVals+": ", word, winfo.score));
      }
    }
  }

  private static boolean DEBUG = false;
  private TrieNode _dict;
  private Map<String, WordInfo> _words;
  private String _requiredLetters;
  private int _maxPrefix;
  private int _maxPostfix;
  private static long _lastTime = 0;
}
