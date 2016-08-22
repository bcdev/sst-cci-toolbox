git pull -f
git checkout -f

rm -rf /home/cartool/car-web-tool/*
cp -r car-web/* /home/cartool/car-web-tool
chmod +x /home/cartool/car-web-tool/run_server_port5000.sh