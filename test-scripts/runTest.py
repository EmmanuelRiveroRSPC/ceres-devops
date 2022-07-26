import yaml
import argparse

parser=argparse.ArgumentParser()

parser.add_argument('-l', '--list', help='Test list file path', default="." )
parser.add_argument('-t', '--test_set', help='Test set to be run', required=True)

args=parser.parse_args()

listPath = args.list
testSet = args.test_set


with open(listPath) as file:
    # The FullLoader parameter handles the conversion from YAML
    # scalar values to Python the dictionary format
    testSet = yaml.load(file, Loader=yaml.FullLoader)

    print(testSet)