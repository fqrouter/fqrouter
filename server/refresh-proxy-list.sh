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
    	--proxy-list "./cnproxy.py 10" | ./update-dns.py
    echo "DONE!"
    date
    sleep 3600
done
