package org.rkilgore;

public class WordFinder {
  public WordFinder(String dictfilename) {
    this._dict = new TrieNode(dictfilename);
  }

  private TrieNode _dict;

  public static void main(String[] args) {
    System.out.println("loading dictionary");
    TrieNode trie = new TrieNode("./scrabble_words.txt");
    System.out.println("loaded.\n");

    for (String candidate : new String[] { "pref", "PREFIX", "prefixe", "prefixED", "prefixede"}) {
      System.out.printf("trie.isPrefix(%s) = %s\n", candidate, trie.isPrefix(candidate));
    }
  }
}
