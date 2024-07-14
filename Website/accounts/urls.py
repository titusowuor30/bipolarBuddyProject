from django.urls import path
from .views import *

urlpatterns = [
    path('signup/', signup_view, name='signup'),
    path('logout/',log_out, name='logout'),
    path('login/', LoginView.as_view(), name='login'),
    path('api/login/', LoginAPIView.as_view(), name='login_api'),
]
