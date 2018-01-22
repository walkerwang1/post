package com.paper.ch2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
public class ListSort {
     public static void main(String[] args){
         ArrayList list = new ArrayList();
         list.add(new Person("lcl",28));
         list.add(new Person("fx",23));
         list.add(new Person("wqx",29));
         Comparator comp = new Mycomparator();
         Collections.sort(list,comp);  
         for(int i = 0;i<list.size();i++){
             Person p = (Person)list.get(i);
             System.out.println(p.getName());
         }   
     }
}
class Mycomparator implements Comparator{  
    public int compare(Object o1,Object o2) {  
        Person p1=(Person)o1;  
        Person p2=(Person)o2;    
       if(p1.age < p2.age)  
           return 1;  
       else  
           return -1;  
       }  
} 
class Person {
	String name;
	int age;
	public Person(String name, int age) {
		this.name = name;
		this.age = age;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}