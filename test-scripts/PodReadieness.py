from time import sleep
import requests
import argparse
from math import floor

#Coloritos!!!!
class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'
    RESETALL = '\033[0m'

parser=argparse.ArgumentParser()

parser.add_argument('-u', '--url', help='URL', required=True)
parser.add_argument('-i', '--inverval', help='time between tests', default="5")
parser.add_argument('-t', '--timeout', help='test duration before exiting', default="300")

args=parser.parse_args()

url=args.url
interval=args.interval
timeout=args.timeout

totalTries=floor(timeout/interval)

if totalTries <= 0:
    print ("timeout must be equal or higher that interval")
    exit(1)

print (f"Will try a total of {totalTries} times, each {interval} secounds")

running = False
print ("Wating for pod to come alive...")
for i in range(totalTries):
    try:
        r = requests.get(url, timeout=1)
    except:
        print("Can't connect...")
        exit(1)
        
    if r.status_code == 200:
        body = r.json
        if body["status"] == "UP":
            running = True
            break
    
    print ("Wating for the aplicacion response")

if running:
    print ("The aplication is ready!!!")
    print ("Have a nice day.")
else:
    print(f"The aplication is still down in given time({timeout})")
    print(f"A total of {bcolors.BOLD}{totalTries}{bcolors.RESETALL} tries {bcolors.FAIL}failed{bcolors.RESETALL}")
    exit (1)