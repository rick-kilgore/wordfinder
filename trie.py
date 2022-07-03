from typing import List, Optional


class TrieNode:
  def __init__(self, ch: chr):
    self.ch: chr = ch
    self.children: List[TrieNode] = []
    self.isword = False

  def __str__(self) -> str:
    return self.strhelper(0).rstrip()

  def strhelper(self, depth: int) -> str:
    prefix = "  " * depth
    s = ""
    if self.children:
      for child in self.children:
        s += f"{prefix}{child.ch}: isword = {child.isword}\n"
        childstr = child.strhelper(depth+1)
        if len(childstr) > 0:
          s += childstr
    return s

  def add(self, ch):
    if not self.children:
      child = TrieNode(ch)
      self.children = [child]
      return child

    for child in self.children:
      if child.ch == ch:
        return child

    child = TrieNode(ch)
    self.children.append(child)
    return child

class TrieResult:
  def __init__(self, isprefix: bool, isword: bool):
    self.isprefix = isprefix
    self.isword = isword

class Trie:
  def __init__(self):
    self.trie = TrieNode('*')

  def __str__(self):
    return str(self.trie)

  def insert(self, word):
    node = self.trie
    for ch in word:
      node = node.add(ch)
    node.isword = True

  def isprefix(self, word) -> TrieResult:
    node = self.trie
    for ch in word:
      if not node.children:
        return TrieResult(False, False)

      found = False
      for child in node.children:
        if child.ch == ch:
          node = child
          found = True
          break

      if not found:
        return TrieResult(False, False)

    return TrieResult(True, node.isword)

