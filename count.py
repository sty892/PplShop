import codecs
from collections import Counter
import re

filename = '2026-04-15_02-57-17_895__03-sign-unresolved-entries.txt'

with codecs.open(filename, 'r', 'utf-8') as f:
    lines = f.readlines()

normalized = []
for line in lines:
    m = re.match(r"^ParsedItemNormalized:\s*(.+)$", line)
    if m:
        normalized.append(m.group(1).strip())

counts = Counter(normalized)
with codecs.open('count_output.txt', 'w', 'utf-8') as out:
    for text, count in counts.most_common(150):
        out.write(f"{count:4d} {text}\n")
print("COUNT DONE")
