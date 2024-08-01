from django.urls import path,include
from .views import *
from rest_framework import routers

router = routers.DefaultRouter()
router.register(r'vitals', VitalsViewSet)
router.register(r'departments', DepartmentViewSet)
router.register(r'tremors', TremorsViewSet, basename='tremors')

urlpatterns=[
    path('api/', include(router.urls)),
    path("", home, name = "home" ), 
    #path("vitals/",VitalsAPIView.as_view(),name='vitals'),
    path("departments/",departments,name='departments'),
    path("contact-us/",contact,name='contact'),

]