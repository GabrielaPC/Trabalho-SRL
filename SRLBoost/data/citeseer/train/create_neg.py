from random import randint

TRAIN_OR_TEST = ['train','test']
for type_file in TRAIN_OR_TEST:
    pos_file = f'/home/gpinheiro/srlboost/SRLBoost/data/drugs/{type_file }/{type_file }_pos.txt'
    neg_file = f'/home/gpinheiro/srlboost/SRLBoost/data/drugs/{type_file }/{type_file }_neg.txt'
    with open(pos_file, 'r') as inp:
        with open(neg_file, 'w') as out:
            pos_lines = inp.readlines()

            for i in range(len(pos_lines)//8):
                rang = [pos_lines[i].index(','),pos_lines[i].index(')')]

                true_cat = int(pos_lines[i][rang[0] + 1:rang[1]])
                
                false_cat = randint(1,7)
                while false_cat == true_cat:
                    false_cat = randint(1,7)

                
                out.write(pos_lines[i].replace(pos_lines[i][rang[0]:rang[1]+1],f',{false_cat})'))

                #print(true_cat, false_cat)




