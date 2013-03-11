import os
import atexit
import signal


def add(hook):
    atexit.register(hook)

    def handle_exit_signals(signum, frame):
        try:
            hook()
        finally:
            signal.signal(signum, signal.SIG_DFL)
            os.kill(os.getpid(), signum) # Rethrow signal

    signal.signal(signal.SIGTERM, handle_exit_signals)
    signal.signal(signal.SIGINT, handle_exit_signals)