export SOCKS_SERVER=127.0.0.1:1080
while true
do
    ./check-proxies.py \
        --proxy-list "./ip-adress.py" \
    	--proxy-list "./cnproxy.py 1" \
    	--proxy-list "./cnproxy.py 2" \
    	--proxy-list "./cnproxy.py 3" \
    	--proxy-list "./cnproxy.py 4" \
    	--proxy-list "./cnproxy.py 5" \
    	--proxy-list "./cnproxy.py 6" \
    	--proxy-list "./cnproxy.py 7" \
    	--proxy-list "./cnproxy.py 8" \
    	--proxy-list "./cnproxy.py 9" \
    	--proxy-list "./cnproxy.py 10" \
    	--proxy-list "./proxynova.py us" \
    	--proxy-list "./proxynova.py br" \
    	--proxy-list "./proxynova.py ru" \
    	--proxy-list "./proxynova.py fr" \
    	--proxy-list "./proxynova.py de" \
    	--proxy-list "./proxynova.py ua" \
    	--proxy-list "./proxynova.py pl" \
    	--proxy-list "./proxynova.py id" \
    	--proxy-list "./spys.py 0" \
    	--proxy-list "./spys.py 1" \
    	--proxy-list "./spys.py 2" \
    	--proxy-list "./spys.py 3" \
    	--proxy-list "./spys.py 4" \
    	--proxy-list "./spys.py 5" \
    	--proxy-list "./hidemyass.py 1" \
    	--proxy-list "./hidemyass.py 2" \
    	--proxy-list "./hidemyass.py 3" \
    	--proxy-list "./hidemyass.py 4" \
    	--proxy-list "./hidemyass.py 5" \
    	--proxy-list "./hidemyass.py 6" \
    	--proxy-list "./hidemyass.py 7" \
    	--proxy-list "./hidemyass.py 8" \
    	--proxy-list "./hidemyass.py 9" \
    	--proxy-list "./hidemyass.py 10" > /tmp/proxies.txt
    ./update-proxy-dns.py < /tmp/proxies.txt && echo "DONE! `date`"
    sleep 1800
done
