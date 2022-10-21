import os
  
types = ['train','test']

for type_files in types:

    input_path = f"PSL/trust-prediction/data/trust-prediction/0/{'learn' if type_files == 'train' else 'eval'}" 
    output_path = f'SRLBoost/data/trust/{type_files}/'

    
    # emptying the files 
    open(f"{output_path}{type_files}_facts.txt", "w").close() 
    open(f"{output_path}{type_files}_pos.txt", "w").close() 
    open(f"{output_path}{type_files}_neg.txt", "w").close() 
        
    def format(word):
        rep = [',','.','"',"'",'`','(',")",':','<','>']
        word = word.replace(' ','_')
        for simb in rep:
            word = word.replace(simb,'')

        return word.strip('_')
    
    def read_and_convert(file_path, name,output_file, is_pos = False):
        with open(file_path, 'r') as f:
            with open(f'{output_path}{output_file}', 'a') as out:
                lines = f.readlines()
                for line in lines:
                    new_line = line.split("\t")
                    prob = 1
                    if len(new_line) == 3:
                        prob = float(new_line.pop())

                    for i in range(len(new_line)): 
                        new_line[i] =  format(new_line[i].strip().lower())
                    if prob >= 0.5:
                        #print(f'{new_line} : {name,prob} \n')
                        out.write(f'{name}({new_line[0]},{new_line[1].strip()}).\n')
                    elif is_pos and prob < 0.2:
                        #print(f'{new_line} : {name,prob} \n')
                        with open(f'{output_path}{output_file.replace("_pos","_neg")}', 'a') as new_out:
                            new_out.write(f'{name}({new_line[0]},{new_line[1].strip()}).\n')
                
                
    
    

    for file in os.listdir(input_path):
        if file.endswith("_obs.txt"):
            file_path = f"{input_path}/{file}"
    
            read_and_convert(file_path, file[:-8].replace('_',''), f"{type_files}_facts.txt")
        elif file.endswith("_truth.txt"):
            file_path = f"{input_path}/{file}"
    
            read_and_convert(file_path, file[:-10].replace('_',''), f"{type_files}_pos.txt", is_pos=True)
        elif file.endswith("_target.txt"):
            file_path = f"{input_path}/{file}"
    
            read_and_convert(file_path, file[:-11].replace('_',''), f"{type_files}_pos.txt", is_pos=True)
        