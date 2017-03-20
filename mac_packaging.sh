# https://github.com/Jorl17/jar2app
rm -rf jar2app
git clone https://github.com/Jorl17/jar2app
sbt clean assembly
cp target/scala-2.12/passzero-assembly-0.0.1.jar jar2app
cd jar2app
python jar2app passzero-assembly-0.0.1.jar passzero -i ../passzero.icns -j "-XstartOnFirstThread -d64" -d "Passzero"
