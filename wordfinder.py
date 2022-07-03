#!/Users/rkilgore/.venv/bin/python

import os
import pickle
import sys
import time
from functools import reduce
from typing import Any, Dict, List, Set

DEBUG = 0

from trie import Trie, TrieResult


class StaticData:
  def __init__(
      self, dictwords: Any,
      necessary_letters: str,
      maxprefix: int, maxpostfix: int,
      use_trie: bool,
  ):
    self.dictwords = dictwords
    self.necessary_letters = necessary_letters
    self.maxprefix = maxprefix
    self.maxpostfix = maxpostfix
    self.use_trie = use_trie


def debug_log(log: str) -> None:
  if DEBUG:
    print(log)

def to_search(letters: str) -> str:
  to_srch = ""
  searched_already = ""
  for ch in letters:
    if ch not in searched_already:
      to_srch += ch
  return to_srch

def has_necessary(word: str, static: StaticData) -> bool:
  for ch in static.necessary_letters:
    if ch not in word:
        return False
  return True


def add_word(word: str, dot_vals: str, words: Dict[str, str]) -> None:
  debug_log(f"        add_word({word}, {dot_vals})")
  words[word] = dot_vals


def recurse(
    depth: int,
    static: StaticData,
    sofar: str, cur_prefix_len: int, cur_postfix_len: int,
    letters: str, template: str, templstarted: bool,
    in_dot_vals: str = "",
) -> Dict[str, str]:

  debug_log(
      f"{'  ' * depth}recurse sofar={sofar} prefix={cur_prefix_len} postfix={cur_postfix_len} "
      f"letters={letters} template={template} tstarted={templstarted} in_dot_vals={in_dot_vals}")
  noLetters = len(letters) == 0
  emptyTemplate = len(template) == 0
  nextTemplateIsLetter = not emptyTemplate and template[0] != '.'
  cantAddPostfix = cur_postfix_len == static.maxpostfix
  if (
      (noLetters and not nextTemplateIsLetter) or
      (emptyTemplate and cantAddPostfix)
  ):
    debug_log(f"    short return: sofar={sofar} letters={letters} templ={template}")
    return {}

  words = {}

  if len(template) > 0:
    # try adding ch from letters to prefix before template
    remaining_letters_required = reduce(lambda tot, ch: tot + (1 if ch == '.' or ch.isupper() else 0), [0] + list(template))
    if not templstarted and cur_prefix_len < static.maxprefix and len(letters) > remaining_letters_required:
      debug_log(f"{'  ' * depth}    prefix: remaining = {remaining_letters_required} for sofar={sofar} letters={letters} templ={template}")
      for ch in to_search(letters):
          newletters = letters.replace(ch, "", 1)
          is_dot = ch == '.'
          chars = 'abcdefghijklmnopqrstuvwxyz' if is_dot else ch
          for xch in chars:
            nextsofar = sofar + xch
            next_dot_vals = in_dot_vals + (xch if is_dot else "")
            result = isprefix(nextsofar, static.dictwords, static.use_trie)
            if result.isword and len(template) == 0 and has_necessary(nextsofar, static):
              add_word(nextsofar, next_dot_vals, words) 
            elif result.isprefix:
              rwords = recurse(depth+1, static, nextsofar, cur_prefix_len + 1, cur_postfix_len, newletters, template, False, next_dot_vals)
              for word, dot_vals in rwords.items():
                add_word(word, dot_vals, words)

  # template letter = '.' - try each letter from letters as match
  if len(template) > 0 and template[0] == '.':
    debug_log(f"{'  ' * depth}    template dot: sofar={sofar} letters={letters} templ={template}")
    for ch in to_search(letters):
      newletters = letters.replace(ch, "", 1)
      newtempl = template[1:]
      is_dot = ch == '.'
      chars = 'abcdefghijklmnopqrstuvwxyz' if is_dot else ch
      for xch in chars:
        nextsofar = sofar + xch
        next_dot_vals = in_dot_vals + (xch if is_dot else "")
        result = isprefix(nextsofar, static.dictwords, static.use_trie)
        if result.isword and len(newtempl) == 0 and has_necessary(nextsofar, static):
          add_word(nextsofar, next_dot_vals, words)
        elif result.isprefix:
          rwords = recurse(depth+1, static, nextsofar, cur_prefix_len, cur_postfix_len, newletters, newtempl, True, next_dot_vals)
          for word, dot_vals in rwords.items():
            add_word(word, dot_vals, words)

  # template letter ch != '.' - add ch.lower() and remove ch.lower() from input letters if ch.isupper()
  elif len(template) > 0:
    debug_log(f"{'  ' * depth}    template NON-dot: sofar={sofar} letters={letters} templ={template}")
    ch = template[0]
    if ch.isupper():
      ch = ch.lower()
      if not ch in letters:
        return words
      letters = letters.replace(ch, "", 1)
    nextsofar = sofar + ch
    newtempl = template[1:]
    result = isprefix(nextsofar, static.dictwords, static.use_trie)
    if result.isword and len(newtempl) == 0 and has_necessary(nextsofar, static):
      add_word(nextsofar, in_dot_vals, words)
    elif result.isprefix:
      rwords = recurse(depth+1, static, nextsofar, cur_prefix_len, cur_postfix_len, letters, newtempl, True, in_dot_vals)
      for word, dot_vals in rwords.items():
        add_word(word, dot_vals, words)

    return words

  # at end, add letters for the postfix
  elif cur_postfix_len < static.maxpostfix:
    debug_log(f"{'  ' * depth}  postfix: sofar={sofar} letters={letters} templ={template}")
    for ch in to_search(letters):
      newletters = letters.replace(ch, "", 1)
      is_dot = ch == '.'
      chars = 'abcdefghijklmnopqrstuvwxyz' if is_dot else ch
      for xch in chars:
        nextsofar = sofar + xch
        next_dot_vals = in_dot_vals + (xch if is_dot else "")
        result = isprefix(nextsofar, static.dictwords, static.use_trie)
        if result.isword and has_necessary(nextsofar, static):
          add_word(nextsofar, next_dot_vals, words)
        if result.isprefix:
          rwords = recurse(depth+1, static, nextsofar, cur_prefix_len, cur_postfix_len + 1, newletters, template, templstarted, next_dot_vals)
          for word, dot_vals in rwords.items():
            add_word(word, dot_vals, words)

    # don't run the code below if we're just adding letters at the end
    return words


last_time = -1.0
def report_time(msg: str) -> float:
  now = time.process_time()
  global last_time
  if last_time >= 0.0:
    print(f"{msg}: elapsed = {now - last_time}")
  else:
    print(f"{msg}: time = {now}")
  last_time = now


def add_dictword(word: str, dictwords: Any, use_trie: bool) -> None:
  if use_trie:
    dictwords.insert(word)
  else:
    dictwords[word] = 1

def isprefix(prefix: str, dictwords: Any, use_trie: bool) -> TrieResult:
  if use_trie:
    return dictwords.isprefix(prefix)
  else:
    return TrieResult(True, prefix in dictwords)

def necessary_chars(letters: str) -> str:
  necessary = ""
  for ch in letters:
    if ch != '.' and ch.isupper():
      necessary += ch.lower()
  return necessary


def load_dict(use_trie: bool) -> Any:
  bn = os.path.basename(sys.argv[0])
  pickle_filename = f"{os.environ.get('HOME')}/wordfinder/{bn}_words.pickle" 
  if use_trie and os.path.exists(pickle_filename):
    with open(pickle_filename, "rb") as picklefile:
      return pickle.load(picklefile)

  else:
    with open(f"{os.environ.get('HOME')}/wordfinder/scrabble_words.txt") as dictfile:
      lines = dictfile.readlines()
      if use_trie:
        dictwords = Trie()
      else:
        dictwords = {}

      for line in lines:
        w = line.lower().strip()
        if len(w) > 0:
          add_dictword(w, dictwords, use_trie)

      if use_trie:
        report_time(f"writing dictionary to {pickle_filename}")
        with open(pickle_filename, "wb") as picklefile:
          pickle.dump(dictwords, picklefile)

      return dictwords


def find_words(static: StaticData, letters: str, template: str) -> None:
  words: Dict[str, str] = recurse(0, static, "", 0, 0, letters, template, False)
  found: Set[str] = sorted(set(
    filter(
      lambda w: w.lower() != template.lower(),
      words.keys()
    )), key=lambda w: len(w), reverse=False)
  for word in found:
    if word in words.keys():
      dot_vals: str = words[word]
      print((f"{dot_vals}: " if len(dot_vals) > 0 else "") + f"{word} {len(word)}")


# main
def main():
  offset = 0
  use_trie = False
  if sys.argv[1] == "-t":
    use_trie = True
    offset = 1

  letters: str = sys.argv[1 + offset]
  template: str = sys.argv[2 + offset] if len(sys.argv) > 2 else ""
  maxprefix: int = int(sys.argv[3 + offset]) if len(sys.argv) > 3+offset else 7
  maxpostfix: int = int(sys.argv[4 + offset]) if len(sys.argv) > 4+offset else 7

  necessary_letters = necessary_chars(letters)
  letters = letters.lower()

  maxdots = 2
  for i, arg in enumerate([letters, template]):
    dotcount = 0
    for ch in arg:
      if ch == '.':
        dotcount += 1
      elif not ch.isalpha():
        print(f"string args must be all letters and '.' chars: {arg}")
        sys.exit(1)
    if i == 0 and dotcount > maxdots:
      print(f"using trie because {dotcount} dots (>{maxdots}) in letters")
      use_trie = True

  report_time("starting")
  dictwords = load_dict(use_trie)

  static: StaticData = StaticData(dictwords, necessary_letters, maxprefix, maxpostfix, use_trie)
  report_time("dictwords loaded")
  find_words(static, letters, template)
  dictwords = None
  report_time("done")


main()
