import socket
import logging

LOGGER = logging.getLogger(__name__)
DEFAULT_PORT = 19840

def start(args):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.setblocking(0)
    s.bind(('0.0.0.0', args.port))

