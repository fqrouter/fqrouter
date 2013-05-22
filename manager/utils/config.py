from ConfigParser import ConfigParser


def read():
    parser = ConfigParser()
    parser.read('/data/data/fq.router/config')
    return parser
