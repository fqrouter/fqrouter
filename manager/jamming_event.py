import collections

events = collections.deque()


def record(event):
    if len(events) > 100:
        events.pop()
    events.append(event)


def list_all():
    return list(events)