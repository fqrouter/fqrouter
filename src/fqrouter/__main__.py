import argparse
import logging
from .node import smuggler, probe

root_parser = argparse.ArgumentParser(description="Qin Fen's Router")
root_parser.add_argument('--debug', action='store_const', const=True, default=False)
sub_parsers = root_parser.add_subparsers()
smuggler_node_parser = sub_parsers.add_parser('smuggler')
smuggler_node_parser.add_argument('--probe-node', required=True)
smuggler_node_parser.add_argument('--queue-number', default=0)
smuggler_node_parser.add_argument('--rfc-3489-server', dest='rfc_3489_servers', action='append')
smuggler_node_parser.set_defaults(handle_by=smuggler.start)
probe_node_parser = sub_parsers.add_parser('probe')
probe_node_parser.add_argument('--port', type=int, default=probe.DEFAULT_PORT)
probe_node_parser.set_defaults(handle_by=probe.start)
args = root_parser.parse_args()

if args.debug:
    logging.basicConfig(level=logging.DEBUG, format='[%(levelname)s] %(asctime)s %(message)s')
args.handle_by(args)