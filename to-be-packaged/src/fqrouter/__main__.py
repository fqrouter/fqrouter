import argparse
import logging
from fqrouter.node import ping_pong, smuggler, impersonator

root_parser = argparse.ArgumentParser(description="Qin Fen's Router")
root_parser.add_argument('--debug', action='store_const', const=True, default=False)
sub_parsers = root_parser.add_subparsers()

ping_parser = sub_parsers.add_parser('ping')
ping_parser.add_argument('host')
ping_parser.add_argument('port', type=int)
ping_parser.set_defaults(handle_by=ping_pong.ping)

pong_parser = sub_parsers.add_parser('pong')
pong_parser.add_argument('port', type=int)
pong_parser.set_defaults(handle_by=ping_pong.pong)

smuggler_parser = sub_parsers.add_parser('smuggler')
smuggler_parser.add_argument('--impersonator-address', required=True)
smuggler_parser.add_argument('--my-public-ip', required=True)
smuggler_parser.set_defaults(handle_by=smuggler.start)

impersonator_parser = sub_parsers.add_parser('impersonator')
impersonator_parser.add_argument('--port', type=int, default=19840)
impersonator_parser.set_defaults(handle_by=impersonator.start)

args = root_parser.parse_args()
logging.basicConfig(level=logging.DEBUG if args.debug else logging.INFO, format='[%(levelname)s] %(asctime)s %(message)s')
args.handle_by(args)