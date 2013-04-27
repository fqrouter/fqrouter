import dns_service
import tcp_service
import full_proxy_service
import lan_service
import wifi
import main


if '__main__' == __name__:
    main.setup_logging()
    dns_service.clean()
    tcp_service.clean()
    full_proxy_service.clean()
    lan_service.clean()
    wifi.clean()
