from contextlib import closing
import json
import socket
import logging

LOGGER = logging.getLogger(__name__)
DEFAULT_PORT = 19840

def start(args):
    with closing(socket.socket(socket.AF_INET, socket.SOCK_DGRAM)) as s:
        s.bind(('0.0.0.0', args.port))
        while True:
            guesses = json.loads(s.recv(4096))
            for guess_id, guess in guesses.items():
                print(guess_id, guess)


def verify_guess(ack_id, ip, port):
    pass



