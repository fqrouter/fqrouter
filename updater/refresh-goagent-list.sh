while true
do
    ./check-goagent-appids.py | ./update-goagent-dns.py && echo "DONE! `date`"
    sleep 900
done
