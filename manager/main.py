import os
import logging
import logging.handlers

import tornado.ioloop
import tornado.template
import tornado.web

import dns_service
import tcp_service
import full_proxy_service
import self_check
import wifi
import jamming_event


ROOT_DIR = os.path.dirname(__file__)
LOG_DIR = '/data/data/fq.router'
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


class JammingEventsHandler(tornado.web.RequestHandler):
    def get(self):
        self.write('<html><body><pre>')
        for event in jamming_event.list_all():
            self.write(event)
            self.write('\n')
        self.write('</pre></body></html>')


application = tornado.web.Application([
    (r'/static/(.*)', tornado.web.StaticFileHandler, {'path': os.path.join(ROOT_DIR, 'static')}),
    (r'/ping', PingHandler),
    (r'/logs', LogsHandler),
    (r'/jamming-events', JammingEventsHandler),
    (r'/self-check', self_check.SelfCheckHandler),
    (r'/wifi', wifi.WifiHandler),
    (r'/', MainHandler)
])


def setup_logging():
    logging.basicConfig(level=logging.INFO)
    handler = logging.handlers.RotatingFileHandler(
        LOG_FILE, maxBytes=1024 * 1024, backupCount=1)
    handler.setFormatter(logging.Formatter(logging.BASIC_FORMAT))
    logging.getLogger().addHandler(handler)


if '__main__' == __name__:
    setup_logging()
    dns_service.run()
    tcp_service.run()
    full_proxy_service.run()
    application.listen(8888, '127.0.0.1')
    tornado.ioloop.IOLoop.instance().start()