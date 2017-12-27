# https://github.com/Jorl17/jar2app
rm -rf jar2app
git clone https://github.com/Jorl17/jar2app
mvn clean package
cp target/passzero-0.0.1-jar-with-dependencies.jar jar2app
cd jar2app
python jar2app passzero-0.0.1-jar-with-dependencies.jar passzero -i ../passzero.icns -j "-XstartOnFirstThread -d64" -d "Passzero"
