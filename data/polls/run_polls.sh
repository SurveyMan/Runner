for i in `seq 1 1`
do
    echo `pwd`
    survey="poll$i.csv"
    java -jar ../../runner.jar --backend=LOCALHOST --breakoff=false $survey &
done
