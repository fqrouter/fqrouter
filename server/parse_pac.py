import sys

domains = set()
for line in sys.stdin.readlines():
    begin = line.find('(*\.|)')
    if -1 == begin:
        continue
    begin += len('(*\.|)')
    end = line.find('")', begin)
    if -1 == end:
        continue
    domains.add(line[begin: end])
for domain in sorted(domains):
    print("'%s'," % domain)