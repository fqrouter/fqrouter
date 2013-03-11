import os
import logging
import sys

import tornado.ioloop
import tornado.template
import tornado.web

import dns_service
import tcp_service


ROOT_DIR = os.path.dirname(__file__)
template_loader = tornado.template.Loader(ROOT_DIR)


class MainHandler(tornado.web.RequestHandler):
    def get(self):
        template = template_loader.load('dashboard.html')
        self.write(template.generate(
            dns_service_status=dns_service.status,
            tcp_service_status=tcp_service.status))


class PingHandler(tornado.web.RequestHandler):
    def get(self):
        self.write('PONG')


application = tornado.web.Application([
    (r'/ping', PingHandler),
    (r'/', MainHandler)
])

if '__main__' == __name__:
    logger = logging.getLogger()
    logger.level = logging.DEBUG
    handler = logging.StreamHandler(os.fdopen(sys.stderr.fileno(), 'w', 0))
    handler.setFormatter(logging.Formatter(logging.BASIC_FORMAT))
    logger.addHandler(handler)
    dns_service.run()
    tcp_service.run()
    application.listen(8888, '127.0.0.1')
    tornado.ioloop.IOLoop.instance().start()