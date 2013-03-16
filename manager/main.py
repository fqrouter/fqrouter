import os
import logging
import logging.handlers

import tornado.ioloop
import tornado.template
import tornado.web

import dns_service
import tcp_service
import self_check


ROOT_DIR = os.path.dirname(__file__)
LOG_DIR = '/sdcard/Android/data/fq.router'
LOG_FILE = os.path.join(LOG_DIR, 'manager.log')
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


class LogsHandler(tornado.web.RequestHandler):
    def get(self):
        self.write('<html><body><pre>')
        with open(LOG_FILE) as f:
            self.write(f.read())
        self.write('</pre></body></html>')


application = tornado.web.Application([
    (r'/static/(.*)', tornado.web.StaticFileHandler, {'path': os.path.join(ROOT_DIR, 'static')}),
    (r'/ping', PingHandler),
    (r'/logs', LogsHandler),
    (r'/self-check', self_check.SelfCheckHandler),
    (r'/', MainHandler)
])


def setup_logging():
    if not os.path.exists(LOG_DIR):
        os.makedirs(LOG_DIR)
    logging.basicConfig(level=logging.INFO)
    handler = logging.handlers.RotatingFileHandler(
        LOG_FILE, maxBytes=1024 * 50, backupCount=3)
    handler.setFormatter(logging.Formatter(logging.BASIC_FORMAT))
    logging.getLogger().addHandler(handler)


if '__main__' == __name__:
    setup_logging()
    dns_service.run()
    tcp_service.run()
    application.listen(8888, '127.0.0.1')
    tornado.ioloop.IOLoop.instance().start()