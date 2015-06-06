d=$1;
bsz=`sudo blockdev --getbsz $d`;
iomin=`sudo blockdev --getiomin $d`;
echo "Block size : $bsz";
echo "IO Min : $iomin";