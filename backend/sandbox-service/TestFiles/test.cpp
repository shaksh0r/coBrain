#include<iostream>
using namespace std;

int main(void){
    int arr[5]={1,2,3,4,5};
    cout<<"Enter Your Number"<<endl;
    int n;
    cin>>n;
    for(int i=0;i<5;i++){
        n += arr[i];
       }
     cout<<n<<endl;
}
