import argparse
import logging
from . import internal_node


root_parser = argparse.ArgumentParser(description="Qin Fen's Router")
root_parser.add_argument('--debug', action='store_const', const=True, default=False)
sub_parsers = root_parser.add_subparsers()
internal_mode_parser = sub_parsers.add_parser('internal')
internal_mode_parser.add_argument('--verify-guesses-via', required=True)
internal_mode_parser.add_argument('--queue-number', default=0)
internal_mode_parser.add_argument('--rfc-3489-server', dest='rfc_3489_servers', action='append')
internal_mode_parser.set_defaults(handle_by=internal_node.start)
args = root_parser.parse_args()

if args.debug:
    logging.basicConfig(level=logging.DEBUG)
args.handle_by(args)