from django.shortcuts import render
from django.http import HttpResponse
from rest_framework.views import APIView
from rest_framework import viewsets
from .serializers import VitalsSerializer
from rest_framework.response import Response
from rest_framework import status
from rest_framework import permissions
from rest_framework.authentication import BasicAuthentication,TokenAuthentication
from .models import Vitals

# Create your views here.
def home(request):
    return render(request, 'core/index.html', {"name":"Wambui"})

# class VitalsAPIView(APIView): #CRUD
#      permission_classes=[permissions.IsAuthenticated]
#      authentication_classes=[BaseAuthentication,TokenAuthentication]

#      def post(self, request, format=None):
#         serializer = VitalsSerializer(data=request.data)
#         if serializer.is_valid():
#             serializer.save()
#             return Response(serializer.data)
#         return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

class VitalsViewSet(viewsets.ModelViewSet):#CRUD
    queryset = Vitals.objects.all()
    serializer_class = VitalsSerializer
    permission_classes=[permissions.IsAuthenticated]
    #authentication_classes=[BasicAuthentication,TokenAuthentication]
