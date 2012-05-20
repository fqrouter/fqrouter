import shlex
import subprocess
import sys

def call(command, capture=False, ignores_error=False, quiet=False, cwd=None):
    if not quiet:
        print('[info] call: %s' % command)
    if capture:
        stdout = subprocess.PIPE
        stderr = subprocess.PIPE
    else:
        stdout, stderr = None, None
    proc = subprocess.Popen(shlex.split(command), stdout=stdout, stderr=stderr, cwd=cwd)
    output, error = proc.communicate()
    status_code = proc.returncode
    if status_code and not ignores_error:
        print('[error] status code: %s' % status_code)
        sys.exit(status_code)
    return output