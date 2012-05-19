import functools
import nfqueue
import socket

def handle_input(payload):
    pass


def monitor_input(queue_number):
    q = nfqueue.queue()
    try:
        q.set_callback(functools.partial(handle_input))
        q.fast_open(queue_number, socket.AF_INET)
        q.set_queue_maxlen(50000)
        q.try_run()
    finally:
        q.unbind(socket.AF_INET)
        q.close()