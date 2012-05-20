# Wrap thread as future object
import functools
import logging
import threading
import thread
import sys
import traceback

LOGGING_LEVEL_TRACE = 5
LOGGER = logging.getLogger(__name__)

def async(func):
# makes func returning Future
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        async = kwargs.pop('async', True)
        logs_exception = kwargs.pop('logs_exception', True)
        if async:
            return Future(func, args, kwargs, logs_exception)
        else:
            return func(*args, **kwargs)

    return wrapper


class ExecutionFailed(Exception):
    def __init__(self, func, args, kwargs, exc_info):
        super(ExecutionFailed, self).__init__(
            'execute %s with args %s and kwargs %s failed: %s' % (func, args, kwargs, exc_info))

# === IMPLEMENTATION ===

class Future(object):
    def __init__(self, func, args, kwargs, logs_exception=True):
        super(Future, self).__init__()
        self.func = func
        self.args = args
        self.kwargs = kwargs
        self.logs_exception = logs_exception
        self.done = threading.Lock()
        self.start()

    def start(self):
        self.done.acquire()
        thread.start_new(self.execute, ())

    def execute(self):
        try:
            if LOGGER.isEnabledFor(LOGGING_LEVEL_TRACE):
                LOGGER.debug('Start executing %s with args %s and kwargs %s' % (self.func, self.args, self.kwargs))
            try:
                self.return_value = self.func(*self.args, **self.kwargs)
            except:
                exc_info = sys.exc_info()
                if self.logs_exception:
                    LOGGER.error('Error: %s' % traceback.format_exception(*exc_info))
                self.exc_info = exc_info
        finally:
            self.done.release()

    def get(self, ignores_error=False):
        with self.done:
            if hasattr(self, 'return_value'):
                return self.return_value
            if hasattr(self, 'exc_info'):
                if ignores_error:
                    LOGGER.debug('Ignored error: %s' % traceback.format_exception(*self.exc_info))
                    return None
                else:
                    raise ExecutionFailed(self.func, self.args, self.kwargs, self.exc_info)
            raise Exception('Future should either have return_value or exc_info')