while true
do
    ./check-goagent-appids.py | ./update-goagent-dns.py && echo "DONE! `date`"
    sleep 450
done
