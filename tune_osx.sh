
# orig default values
# kern.maxfilesperproc: 10240
# kern.maxfiles: 12288
# net.inet.ip.portrange.first: 49152
# ulimit -n: 256
# ulimit -S -n: 1024

#http://docs.basho.com/riak/latest/cookbooks/Open-Files-Limit/#Mac-OS-X

sudo sysctl -w kern.maxfilesperproc=262144
sudo sysctl -w kern.maxfiles=262144
sudo sysctl -w net.inet.ip.portrange.first=1024

sudo launchctl limit maxfiles 1000000 1000000
#ulimit -n 262144
#sudo ulimit -S -n 50000
#ulimit -s 16384


ulimit -S 65536