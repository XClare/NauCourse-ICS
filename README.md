# NauCourse-ICS
[![Build Status](https://api.travis-ci.com/XClare/NauCourse-ICS.svg?branch=master)](https://travis-ci.com/XClare/NauCourse-ICS)

## Description:  
This is a course converter for Nanjing Audit University.  
You can convert courses from jwc.nau.edu.cn into .ics iCalendar File.  

## Requirements:
Jre 1.8 or above  
Internet connection  

## Usage: 
Interactive mode:  
```
    java -jar [Name of the jar file]
```
  
You can also use args mode:  
```
    java -jar [Name of the jar file] [User Id] [User Password] [Data Type]
```
Data Type can be:  
- 1 : This term course data  
- 2 : Next term course data  
- 3 : This term exam data  
   
Such as:  
```
    java -jar NauCourse-ICS.jar XXXXXXXX XXXXXX 1
```
   
If you choose "Next term course data", you should also add term start and end date args.  
```
    java -jar [Name of the jar file] [User Id] [User Password] 3 [Term start date] [Term end date]
```
Such as:  
```
    java -jar NauCourse-ICS.jar XXXXXXXX XXXXXX 3 2020-02-17 2020-07-05
```
Remember that term start date must be Monday and end date must be Sunday!


## Attention:  
This is just for learning and testing.   
Anyone who use this as hacking tools is not agreed by us.   
All the losses are irrelevant to us.

## License:
    GNU 3.0

    NauCourse-ICS
    Copyright (C) 2020  XClare

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
