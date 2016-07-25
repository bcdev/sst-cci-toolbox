import os
import shutil
import sys

input_dir = '/usr/local/data/SST-CCI/amsre'
target_dir = '/usr/local/data/DELETE'

def create_target_path(token, target_dir):
    token_list = str.split(token, '.')
    assembly_path = target_dir
    for path_element in token_list:
        assembly_path = os.path.join(assembly_path, path_element)
    return assembly_path

def move_all_files(src_dir, target_dir):
    directory_content = os.listdir(src_dir)
    for file in directory_content:
        src_file = os.path.join(src_dir, file)
        if not os.path.isfile(src_file):
            continue

        target_file = os.path.join(target_dir, file)
        print 'moving ' + src_file + ' to ' + target_file
        shutil.move(src_file, target_file)
        print 'success ...'



if __name__ == "__main__":
    if not os.path.isdir(input_dir):
        print("input path " + input_dir + " does not exist")
        sys.exit(1)

    if not os.path.isdir(target_dir):
        os.mkdir(target_dir)

    directory_content = os.listdir(input_dir)
    for token in directory_content:
        token_path = os.path.join(input_dir, token)
        if os.path.isdir(token_path):
            target_path = create_target_path(token, target_dir)
            os.makedirs(target_path)
            move_all_files(token_path, target_path)


