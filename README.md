# Charlotte
A java web-spider.

This program takes a given seed URL (in this case [rockhopper.us](http://www.rockhopper.us)) and conducts either a breadth-first search or depth-first search on the structure of pages linked from the seed site. In this way the structure of the internet in the vicinity of a particular webpage can be understood.

####Database Structure
The results gathered by Charlotte are stored in a MySQL database (configurable in the code, but curently scrubbed of login credentials for my own database). In my implementation I used a [WAMP](http://www.wampserver.com/en/) stack, although many other common database solutions could be easily implemented into this code. The structure of the internet is stored in graph form using two tables. The first keys an automatically incrementing integer ID to each unique URL encountered, and the second stores tuples of ID numbers which represent an edge in the graph. Together, these two tables can be used to reconstruct the structure of the internet as parsed by Charlotte. 

####Acknowledgments
Charlotte's DFS code was learned from [ryanlr's excellent tutorial](http://www.programcreek.com/2012/12/how-to-make-a-web-crawler-using-java/). [JSoup](http://jsoup.org/) was also invaluable in implementing this project. I brainstormed Charlotte's BFS method at the recent [PennApps XII](http://2015f.pennapps.com/) in conjunction with [Charles Nickerson](https://github.com/CharlesNickerson). Equipment used and code implemented are my own.
