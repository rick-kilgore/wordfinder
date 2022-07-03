#!/Users/rkilgore/.venv/bin/python

import os
import pickle
import sys
import time
from functools import reduce
from typing import Dict, List, Set

DEBUG = 0
USE_TRIE = False

from trie import Trie, TrieResult

# main
offset = 0
if sys.argv[1] == "-t":
  USE_TRIE = True
  offset = 1
letters = sys.argv[1 + offset]
template = sys.argv[2 + offset] if len(sys.argv) > 2 else ""
maxprefix = int(sys.argv[3 + offset]) if len(sys.argv) > 3+offset else 7
maxpostfix = int(sys.argv[4 + offset]) if len(sys.argv) > 4+offset else 7

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
    USE_TRIE = True

dictwords = {}
if USE_TRIE:
  dictwords = Trie()

def add_dictword(word: str) -> None:
  global dictwords
  if USE_TRIE:
    dictwords.insert(word)
  else:
    dictwords[word] = 1

def isprefix(prefix: str) -> TrieResult:
  global dictwords
  if USE_TRIE:
    return dictwords.isprefix(prefix)
  else:
    return TrieResult(True, prefix in dictwords)


def debug_log(log: str) -> None:
  if DEBUG:
    print(log)

def necessary_chars(letters: str) -> str:
  necessary = ""
  for ch in letters:
    if ch != '.' and ch.isupper():
      necessary += ch.lower()
  return necessary

necessary = necessary_chars(letters)
letters = letters.lower()

def nextletters(letters: str, chosen: int) -> str:
    result = ""
    if chosen > 0:
      result += letters[:chosen]
    if chosen < len(letters) - 1:
      result += letters[chosen+1:]
    return result

def has_necessary(word: str) -> bool:
  global necessary
  for ch in necessary:
    if ch not in word:
        return False
  return True


def to_search(letters: str) -> str:
  to_srch = ""
  searched_already = ""
  for ch in letters:
    if ch not in searched_already:
      to_srch += ch
  return to_srch


def add_word(word: str, dot_vals: str, words: Dict[str, str]) -> None:
  debug_log(f"        add_word({word}, {dot_vals})")
  words[word] = dot_vals


def recurse(sofar: str, prefix: int, postfix: int, letters: str, template: str, templstarted: bool, in_dot_vals: str = "") -> Dict[str, str]:

  debug_log(f"recurse sofar={sofar} prefix={prefix} postfix={postfix} letters={letters} template={template} tstarted={templstarted} in_dot_vals={in_dot_vals}")
  noLetters = len(letters) == 0
  emptyTemplate = len(template) == 0
  nextTemplateIsLetter = not emptyTemplate and template[0] != '.'
  cantAddPostfix = postfix == maxpostfix
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
    if not templstarted and prefix < maxprefix and len(letters) > remaining_letters_required:
      debug_log(f"    prefix: remaining = {remaining_letters_required} for sofar={sofar} letters={letters} templ={template}")
      for ch in to_search(letters):
          newletters = letters.replace(ch, "", 1)
          is_dot = ch == '.'
          chars = 'abcdefghijklmnopqrstuvwxyz' if is_dot else ch
          for xch in chars:
            nextsofar = sofar + xch
            next_dot_vals = in_dot_vals + (xch if is_dot else "")
            result = isprefix(nextsofar)
            if result.isword and len(template) == 0 and has_necessary(nextsofar):
              add_word(nextsofar, next_dot_vals, words) 
            elif result.isprefix:
              rwords = recurse(nextsofar, prefix + 1, postfix, newletters, template, False, next_dot_vals)
              for word, dot_vals in rwords.items():
                add_word(word, dot_vals, words)

    # template letter = '.' - try each letter from letters as match
    if len(template) > 0 and template[0] == '.':
      debug_log(f"    template dot: sofar={sofar} letters={letters} templ={template}")
      for ch in to_search(letters):
        newletters = letters.replace(ch, "", 1)
        newtempl = template[1:]
        is_dot = ch == '.'
        chars = 'abcdefghijklmnopqrstuvwxyz' if is_dot else ch
        for xch in chars:
          nextsofar = sofar + xch
          next_dot_vals = in_dot_vals + (xch if is_dot else "")
          result = isprefix(nextsofar)
          if result.isword and len(newtempl) == 0 and has_necessary(nextsofar):
            add_word(nextsofar, next_dot_vals, words)
          elif result.isprefix:
            rwords = recurse(nextsofar, prefix, postfix, newletters, newtempl, True, next_dot_vals)
            for word, dot_vals in rwords.items():
              add_word(word, dot_vals, words)

    # template letter ch != '.' - add ch.lower() and remove ch.lower() from input letters if ch.isupper()
    elif len(template) > 0:
      debug_log(f"    template NON-dot: sofar={sofar} letters={letters} templ={template}")
      ch = template[0]
      if ch.isupper():
        ch = ch.lower()
        letters = letters.replace(ch, "", 1)
      nextsofar = sofar + ch
      newtempl = template[1:]
      result = isprefix(nextsofar)
      if result.isword and len(newtempl) == 0 and has_necessary(nextsofar):
        add_word(nextsofar, in_dot_vals, words)
      elif result.isprefix:
        rwords = recurse(nextsofar, prefix, postfix, letters, newtempl, True, in_dot_vals)
        for word, dot_vals in rwords.items():
          add_word(word, dot_vals, words)

    return words

  # at end, add letters for the postfix
  elif postfix < maxpostfix:
    for ch in to_search(letters):
      newletters = letters.replace(ch, "", 1)
      is_dot = ch == '.'
      chars = 'abcdefghijklmnopqrstuvwxyz' if is_dot else ch
      for xch in chars:
        nextsofar = sofar + xch
        next_dot_vals = in_dot_vals + (xch if is_dot else "")
        result = isprefix(nextsofar)
        if result.isword and has_necessary(nextsofar):
          add_word(nextsofar, next_dot_vals, words)
        elif result.isprefix:
          rwords = recurse(nextsofar, prefix, postfix + 1, newletters, template, templstarted, next_dot_vals)
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


def load_dict():
  global dictwords
  pickle_filename = f"{os.environ.get('HOME')}/findwords/{sys.argv[0]}_words.pickle" 
  if USE_TRIE and os.path.exists(pickle_filename):
    with open(pickle_filename, "rb") as picklefile:
      dictwords = pickle.load(picklefile)

  else:
    with open(f"{os.environ.get('HOME')}/findwords/scrabble_words.txt") as dictfile:
      lines = dictfile.readlines()
      for line in lines:
        w = line.lower().strip()
        if len(w) > 0:
          add_dictword(w)

      if USE_TRIE:
        report_time(f"writing dictionary to {pickle_filename}")
        with open(pickle_filename, "wb") as picklefile:
          pickle.dump(dictwords, picklefile)


def find_words() -> None:
  words: Dict[str, str] = recurse("", 0, 0, letters, template, False)
  found: Set[str] = sorted(set(
    filter(
      lambda w: w.lower() != template.lower(),
      words.keys()
    )), key=lambda w: len(w), reverse=False)
  for word in found:
    if word in words.keys():
      dot_vals: str = words[word]
      print((f"{dot_vals}: " if len(dot_vals) > 0 else "") + f"{word} {len(word)}")


report_time("starting")
load_dict()
report_time("dictwords loaded")
find_words()
dictwords = None
report_time("done")

