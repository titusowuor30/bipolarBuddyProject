from django.urls import path,include
from .views import *
from rest_framework.routers import DefaultRouter

router = DefaultRouter()
router.register(r'patients/signup', PatientSignupViewSet, basename='patient-signup')

urlpatterns = [
    path('api/', include(router.urls)),
    path('signup/', signup_view, name='signup'),
    path('logout/',log_out, name='logout'),
    path('login/', LoginView.as_view(), name='login'),
    path('api/login/', LoginAPIView.as_view(), name='login_api'),
]
