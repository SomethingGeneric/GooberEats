[[ ! "$EUID" == "0" ]] && echo "Run as root!" && exit 1

here=$(pwd)

cp goobereats.service installtmp
sed -i "s|BOING|$here|g" installtmp
install -m 644 installtmp /etc/systemd/system/goobereats.service
rm installtmp

systemctl daemon-reload
systemctl enable goobereats
systemctl start goobereats