import functools
import socket
import sys
import datetime

pool = []

def async(func):
    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        read, dispose = func(*args, **kwargs)
        future = Future(read, dispose)
        pool.append(future)
        return future

    return wrapper


def read_pool(waiting_for):
    while pool:
        for future in list(pool):
            if future._read():
                pool.remove(future)
                if waiting_for == future:
                    return


class Future(object):
    def __init__(self, read, dispose):
        self.read = read
        self.dispose = dispose
        self.started_at = datetime.datetime.now()

    def get(self, ignores_error=False):
        read_pool(self)
        if hasattr(self, 'result'):
            return self.result
        elif ignores_error:
            return None
        elif hasattr(self, 'exc_info'):
            raise ReadFailed(self.exc_info)
        elif hasattr(self, 'timeout_at'):
            raise Exception
        else:
            raise Exception('Internal Error')

    def _read(self):
        done = self._just_read()
        if done:
            self.dispose()
        return done

    def _just_read(self):
        try:
            self.result = self.read()
        except socket.error:
            now = datetime.datetime.now()
            if now - self.started_at > datetime.timedelta(seconds=5):
                self.timeout_at = now
                return True
            return False
        except:
            self.exc_info = sys.exc_info()
        return True



class ReadFailed(Exception):
    def __init__(self, exc_info):
        super(ReadFailed, self).__init__()
        self.exc_info = exc_info


class Timeout(Exception):
    def __init__(self, started_at, timeout_at):
        super(Timeout, self).__init__()
        self.started_at = started_at
        self.timeout_at = timeout_at