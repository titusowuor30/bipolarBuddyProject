from django.urls import path,include
from .views import *
from rest_framework import routers

router = routers.DefaultRouter()
router.register(r'vitals', VitalsViewSet)

urlpatterns=[
    path('api/', include(router.urls)),
    path("", home, name = "home" ), 
    #path("vitals/",VitalsAPIView.as_view(),name='vitals'),
]