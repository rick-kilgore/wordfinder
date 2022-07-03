#!/Users/rkilgore/.venv/bin/python

from io import TextIOWrapper


def inline_pylib(libfilename: str, outfile: TextIOWrapper) -> None:
  with open(libfilename, "r") as libfile:
    outfile.writelines(libfile.readlines());

with open("./findwords.py", "r") as infile:
  lines = infile.readlines()
  with open("./fw.py", "w") as outfile:
    for line in lines:
      if line.startswith("from trie import"):
        inline_pylib("./trie.py", outfile)
      else:
        outfile.write(line)

