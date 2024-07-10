from django.shortcuts import render
from django.http import HttpResponse

#function based and class based view functions
def home(request):
    return HttpResponse("You hit the home page. Coming soon!")
